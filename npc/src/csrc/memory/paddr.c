#include <paddr.h>

uint8_t pmem[MEM_SIZE] __attribute((aligned(4096))) = {};

uint8_t *guest_to_host(uint64_t paddr)
{
    return pmem + paddr - MEM_BASE;
}
uint64_t host_to_guest(uint8_t *haddr)
{
    return haddr - pmem + MEM_BASE;
}

uint64_t host_read(void *addr, int len)
{
    switch (len)
    {
        case 1: return *(uint8_t  *)addr;
        case 2: return *(uint16_t *)addr;
        case 4: return *(uint32_t *)addr;
        case 8: return *(uint64_t *)addr;
        default: assert(0);
    }
}

void host_write(void *addr, int len, uint64_t data)
{
    switch (len)
    {
        case 1: *(uint8_t  *)addr = data; return;
        case 2: *(uint16_t *)addr = data; return;
        case 4: *(uint32_t *)addr = data; return;
        case 8: *(uint64_t *)addr = data; return;
        default: assert(0);
    }
}

uint64_t paddr_read(uint64_t addr, int len)
{
    return host_read(guest_to_host(addr), len);
}

void paddr_write(uint64_t addr, int len, uint64_t data)
{
    host_write(guest_to_host(addr), len, data);
}

void init_mem()
{
    uint32_t *p = (uint32_t *) pmem;
    for (int i = 0; i < (int) (MEM_SIZE / sizeof(p[0])); ++ i)
    {
        p[i] = rand();
    }
}

int in_pmem(uint64_t addr)
{
    return addr - MEM_BASE < MEM_SIZE;
}

uint64_t paddr_read(uint64_t addr, int len)
{
    if (in_pmem(addr))
    {
        return pmem_read(addr, len);
    }
    else
    {
        assert(0);
    }
}

void paddr_write(uint64_t addr, int len, uint64_t data)
{
    if (in_pmem(addr))
    {
        paddr_write(addr, len, data);
    }
    else
    {
        assert(0);
    }
}
