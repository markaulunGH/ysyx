#include <device.h>
#include <utils.h>

word_t mmio_read(paddr_t addr, int len)
{
    if (addr == RTC_ADDR)
    {
        return get_time();
    }
}

void mmio_write(paddr_t addr, int len, word_t data)
{
    if (addr == SERIAL_PORT)
    {
        putchar(data);
    }
}

void init_device()
{

}
