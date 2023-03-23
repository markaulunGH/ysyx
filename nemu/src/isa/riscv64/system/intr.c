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
#include <../local-include/reg.h>

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  csr_mstatus = (SEXT(BITS(csr_mstatus, 63, 13), 51) << 13) | (3 << 11) | (BITS(csr_mstatus, 10, 8) << 8) |
                (BITS(csr_mstatus, 3, 3) << 7) | (BITS(csr_mstatus, 6, 4) << 4) | (BITS(csr_mstatus, 2, 0));
  csr_mepc = epc;
  csr_mcause = NO;
  return csr_mtvec;
}

word_t isa_mret() {
  csr_mstatus = (SEXT(BITS(csr_mstatus, 63, 13), 51) << 13) | (BITS(csr_mstatus, 10, 8) << 8) | (1 << 7) |
                (BITS(csr_mstatus, 6, 4) << 4) | (BITS(csr_mstatus, 7, 7) << 3) | (BITS(csr_mstatus, 2, 0));
  return csr_mepc + 4;
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
