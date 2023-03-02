/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <isa.h>
#include <memory/paddr.h>

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum {
  TK_NOTYPE = 256,
  TK_NUM,
  TK_REG,
  TK_EQ,
  TK_NEQ,
  TK_AND,
  TK_DEREF,
};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {
  {" +", TK_NOTYPE},
  {"\\+", '+'},
  {"-", '-'},
  {"\\*", '*'},
  {"/", '/'},
  {"\\(", '('},
  {"\\)", ')'},
  {"0x([0-9]|[a-f]|[A-F])+|[0-9]+", TK_NUM},
  {"\\$([a-z]|[0-9])+", TK_REG},
  {"==", TK_EQ},
  {"!=", TK_NEQ},
  {"&&", TK_AND},
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        switch (rules[i].token_type) {
          case TK_NOTYPE: continue;
          default:
            strncpy(tokens[nr_token].str, substr_start, substr_len);
            tokens[nr_token].type = rules[i].token_type;
            if (strncmp(tokens[nr_token].str, "0x", 2) == 0 && tokens[nr_token].type == TK_NUM) {
              word_t val;
              sscanf(tokens[nr_token].str, "%lx\n", &val);
              sprintf(tokens[nr_token].str, "%ld\n", val);
            }
            ++ nr_token;
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

bool check_parantheses(int l, int r) {
  if (tokens[l].type != '(') {
    return false;
  }
  int bracket = 0;
  for (int i = l; i < r; ++ i) {
    if (tokens[i].type == '(') {
      ++ bracket;
    }
    else if (tokens[i].type == ')') {
      -- bracket;
    }
    if (bracket == 0 && i != r - 1) {
      return false;
    }
  }
  return true;
}

word_t eval(int l, int r, bool *success) {
  if (l >= r) {
    *success = false;
    return 0;
  }
  else if (l == r - 1) {
    if (tokens[l].type == TK_NUM) {
      return atoi(tokens[l].str);
    }
    else if (tokens[l].type == TK_REG) {
      return isa_reg_str2val(tokens[l].str + 1, success);
    }
    else {
      *success = false;
      return 0;
    }
  }
  else if (check_parantheses(l, r)) {
    return eval(l + 1, r - 1, success);
  }
  else {
    int op = l;
    int bracket = 0;
    int pri = 2;
    for (int i = l; i < r; ++ i) {
      if (tokens[i].type == '(') {
        ++ bracket;
      }
      else if (tokens[i].type == ')') {
        -- bracket;
      }
      if (bracket > 0) {
        continue;
      }
      
      if (tokens[i].type == '+' || tokens[i].type == '-') {
        pri = 0;
        op = i;
      }
      if (pri == 1 && (tokens[i].type == '*' || tokens[i].type == '/')) {
        op = i;
      }
      if (pri == 2 && tokens[i].type == TK_DEREF) {
        op = i;
      }
    }

    word_t val1 = 0;
    if (tokens[op].type != TK_DEREF) {
      val1 = eval(l, op, success);
    }
    word_t val2 = eval(op + 1, r, success);

    switch (tokens[op].type) {
      case '+'      : return val1 + val2;
      case '-'      : return val1 - val2;
      case '*'      : return val1 * val2;
      case '/'      : return val1 / val2;
      case TK_EQ    : return val1 == val2;
      case TK_NEQ   : return val1 != val2;
      case TK_AND   : return val1 && val2;
      case TK_DEREF : return *(word_t*) guest_to_host(val2);
      default       : assert(0);
    }
  }
  return 0;
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  for (int i = 0; i < nr_token; ++ i) {
    if (tokens[i].type == '*' && (i == 0 || (tokens[i - 1].type != TK_NUM && tokens[i - 1].type != ')'))) {
      tokens[i].type = TK_DEREF;
    }
  }

  return eval(0, nr_token, success);
}
