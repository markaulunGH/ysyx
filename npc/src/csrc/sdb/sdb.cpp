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

#include <paddr.h>
#include <malloc.h>
#include <readline/readline.h>
#include <readline/history.h>
#include <sdb.h>
#include <cpu.h>

NPCState npc_state;

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char *rl_gets()
{
    static char *line_read = NULL;

    if (line_read)
    {
        free(line_read);
        line_read = NULL;
    }

    line_read = readline("(npc) ");

    if (line_read && *line_read)
    {
        add_history(line_read);
    }

    return line_read;
}

static int cmd_c(char *args)
{
    cpu_exec(-1);
    return 0;
}

static int cmd_q(char *args)
{
    npc_state.state = NPC_QUIT;
    return -1;
}

static int cmd_si(char *args)
{
    int step = args == NULL ? 1 : atoi(args);
    cpu_exec(step);
    return 0;
}

static int cmd_info(char *args)
{
    if (strcmp(args, "r") == 0)
    {
        reg_display();
    }
    else if (strcmp(args, "w") == 0)
    {
        display_wp();
    }
    else
    {
        printf("info r: information of registers\n");
        printf("info w: information of watchpoints\n");
    }
    return 0;
}

static int cmd_x(char *args)
{
    char *num_str = strtok(args, " ");
    int num = atoi(num_str);
    bool success = true;
    word_t val = expr(args + strlen(num_str) + 1, &success);
    if (success == true)
    {
        for (int i = 0; i < num; ++i)
        {
            printf("0x%-18lx", *(word_t *)guest_to_host(val + i * 8));
            if (i % 8 == 7)
            {
                printf("\n");
            }
        }
        printf("\n");
    }
    else
    {
        printf("Invalid expression\n");
    }
    return 0;
}

static int cmd_p(char *args)
{
    bool success = true;
    word_t val = expr(args, &success);
    if (success == true)
    {
        printf("%ld\n", val);
    }
    else
    {
        printf("Invalid expression\n");
    }
    return 0;
}

static int cmd_w(char *args)
{
    bool success = true;
    new_wp(args, &success);
    return 0;
}

static int cmd_d(char *args)
{
    free_wp(atoi(args));
    return 0;
}

static int cmd_help(char *args);

static struct
{
    const char *name;
    const char *description;
    int (*handler)(char *);
} cmd_table[] = {
    {"help", "Display information about all supported commands", cmd_help},
    {"c", "Continue the execution of the program", cmd_c},
    {"q", "Exit NPC", cmd_q},
    {"si", "Step over", cmd_si},
    {"info", "Print state of program", cmd_info},
    {"x", "Scan the memory", cmd_x},
    {"p", "Print the value of expression", cmd_p},
    {"w", "Watch the value of expression", cmd_w},
    {"d", "Delete watch point", cmd_d},
};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args)
{
    /* extract the first argument */
    char *arg = strtok(NULL, " ");
    int i;

    if (arg == NULL)
    {
        /* no argument given */
        for (i = 0; i < NR_CMD; i++)
        {
            printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        }
    }
    else
    {
        for (i = 0; i < NR_CMD; i++)
        {
            if (strcmp(arg, cmd_table[i].name) == 0)
            {
                printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
                return 0;
            }
        }
        printf("Unknown command '%s'\n", arg);
    }
    return 0;
}

void sdb_set_batch_mode()
{
    is_batch_mode = true;
}

void sdb_mainloop()
{
    if (is_batch_mode)
    {
        cmd_c(NULL);
        return;
    }

    for (char *str; (str = rl_gets()) != NULL;)
    {
        char *str_end = str + strlen(str);

        /* extract the first token as the command */
        char *cmd = strtok(str, " ");
        if (cmd == NULL)
        {
            continue;
        }

        /* treat the remaining string as the arguments,
         * which may need further parsing
         */
        char *args = cmd + strlen(cmd) + 1;
        if (args >= str_end)
        {
            args = NULL;
        }

#ifdef CONFIG_DEVICE
        extern void sdl_clear_event_queue();
        sdl_clear_event_queue();
#endif

        int i;
        for (i = 0; i < NR_CMD; i++)
        {
            if (strcmp(cmd, cmd_table[i].name) == 0)
            {
                if (cmd_table[i].handler(args) < 0)
                {
                    return;
                }
                break;
            }
        }

        if (i == NR_CMD)
        {
            printf("Unknown command '%s'\n", cmd);
        }
    }
}

void init_sdb(bool batch)
{
    if (batch)
    {
        sdb_set_batch_mode();
    }

    /* Compile the regular expressions. */
    init_regex();

    /* Initialize the watchpoint pool. */
    init_wp_pool();
}
