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

#include <sdb.h>

#define NR_WP 32

struct WP
{
    int NO;
    WP *next;
    char e[64];
    word_t val;
};

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool()
{
    int i;
    for (i = 0; i < NR_WP; i++)
    {
        wp_pool[i].NO = i;
        wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
    }

    head = NULL;
    free_ = wp_pool;
}

void display_wp()
{
    for (WP *x = head; x != NULL; x = x->next)
    {
        printf("%d: %s\n", x->NO, x->e);
    }
}

void new_wp(char *e, bool *success)
{
    word_t val = expr(e, success);
    if (*success == false)
    {
        return;
    }
    if (free_ == NULL)
    {
        printf("Fail to add watchpoints, please delete some first\n");
        *success = false;
        return;
    }
    WP *x = free_;
    free_ = free_->next;
    x->next = head;
    head = x;
    strcpy(x->e, e);
    x->val = val;
    printf("Watchpoint %d: %s\n", x->NO, x->e);
}

void free_wp(int NO)
{
    if (head == NULL)
    {
        return;
    }
    for (WP *x = head, *y = NULL; x != NULL; y = x, x = x->next)
    {
        if (x->NO == NO)
        {
            if (y)
            {
                y->next = x->next;
            }
            x->next = free_;
            free_ = x;
            return;
        }
    }
}

bool scan_wp()
{
    bool success;
    for (WP *x = head; x != NULL; x = x->next)
    {
        word_t val = expr(x->e, &success);
        if (val != x->val)
        {
            printf("Watchpoint %d: %s\n", x->NO, x->e);
            printf("Old value = %ld\n", x->val);
            printf("New value = %ld\n", val);
            x->val = val;
            return true;
        }
    }
    return false;
}
