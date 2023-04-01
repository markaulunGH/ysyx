#ifndef __DEVICE_H__
#define __DEVICE_H__

#include <common.h>

word_t mmio_read(paddr_t addr, int len);
void mmio_write(paddr_t addr, int len, word_t data);

void init_device();

#define SERIAL_PORT 0xa00003f8
#define RTC_ADDR    0xa0000048

#endif
