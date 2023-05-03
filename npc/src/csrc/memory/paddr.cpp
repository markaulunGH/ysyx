#include <stdio.h>
#include <paddr.h>
#include <config.h>
#include <log.h>
#include <sdb.h>

uint8_t pmem[MEM_SIZE] __attribute((aligned(4096))) = {};

uint8_t *guest_to_host(paddr_t paddr)
{
    return pmem + paddr - MEM_BASE;
}

paddr_t host_to_guest(uint8_t *haddr)
{
    return haddr - pmem + MEM_BASE;
}

word_t host_read(void *addr, int len)
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

void host_write(void *addr, int len, word_t data)
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

word_t pmem_read(paddr_t addr, int len)
{
    return host_read(guest_to_host(addr), len);
}

void pmem_write(paddr_t addr, int len, word_t data)
{
    host_write(guest_to_host(addr), len, data);
}

void init_mem()
{
    // uint32_t *p = (uint32_t *) pmem;
    // for (int i = 0; i < (int) (MEM_SIZE / sizeof(p[0])); ++ i)
    // {
    //     p[i] = rand();
    // }
}

int load_image(char *img_file)
{
    FILE *fp = fopen(img_file, "rb");
    fseek(fp, 0, SEEK_END);
    int size = ftell(fp);
    fseek(fp, 0, SEEK_SET);
    assert(fread(pmem, size, 1, fp));
    fclose(fp);
    return size;
}

word_t paddr_read(paddr_t addr, int len)
{
    if (in_pmem(addr))
    {
        return pmem_read(addr, len);
    }
    else
    {
        printf("paddr_read: addr = %x, len = %d\n", addr, len);
        npc_state.state = NPC_ABORT;
    }
}

void paddr_write(paddr_t addr, int len, word_t data)
{
    if (in_pmem(addr))
    {
        pmem_write(addr, len, data);
    }
    else
    {
        printf("paddr_write: addr = %x, len = %d, data = %x\n", addr, len, data);
        npc_state.state = NPC_ABORT;
    }
}
