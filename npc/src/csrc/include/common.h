#ifndef __COMMON_H__
#define __COMMON_H__

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <assert.h>
#include <log.h>

typedef uint64_t word_t;
typedef int64_t sword_t;

typedef word_t vaddr_t;
typedef uint64_t paddr_t;

#define ARRLEN(arr) (int)(sizeof(arr) / sizeof(arr[0]))

#endif
