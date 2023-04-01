#include <device.h>
#include <utils.h>
#include <SDL2/SDL.h>

SDL_Renderer *renderer = NULL;
SDL_Texture *texture = NULL;

char vmem[SCREEN_W * SCREEN_H * 4];
uint32_t vgactl_port_base[2];

void init_screen()
{
    SDL_Window *window = NULL;
    char title[128];
    sprintf(title, "riscv64-NPC");
    SDL_Init(SDL_INIT_VIDEO);
    SDL_CreateWindowAndRenderer(SCREEN_W * 2, SCREEN_H * 2, 0, &window, &renderer);
    SDL_SetWindowTitle(window, title);
    texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STATIC, SCREEN_W, SCREEN_H);
}

void update_screen()
{
    SDL_UpdateTexture(texture, NULL, vmem, SCREEN_W * sizeof(uint32_t));
    SDL_RenderClear(renderer);
    SDL_RenderCopy(renderer, texture, NULL, NULL);
    SDL_RenderPresent(renderer);
}

void vga_update_screen()
{
    if (vgactl_port_base[1] == 1)
    {
        update_screen();
        vgactl_port_base[1] = 0;
    }
}

void init_vga()
{
    vgactl_port_base[0] = (SCREEN_W << 16) | SCREEN_H;
    init_screen();
}

word_t mmio_read(paddr_t addr, int len)
{
    if (addr == RTC_ADDR)
    {
        return get_time();
    }
    else if (addr == VGACTL_ADDR)
    {
        return vgactl_port_base[0];
    }
    printf("%p\n", addr);
    assert(0);
}

void mmio_write(paddr_t addr, int len, word_t data)
{
    if (addr == SERIAL_PORT)
    {
        putchar(data);
    }
    else if (addr == VGACTL_ADDR + 4)
    {
        vgactl_port_base[1] = data;
        vga_update_screen();
    }
    else if (FB_ADDR <= addr && addr < FB_ADDR + SCREEN_W * SCREEN_H * 4)
    {
        switch(len)
        {
            case 1: *(uint8_t  *)(vmem + (addr - FB_ADDR)) = data; break;
            case 2: *(uint16_t *)(vmem + (addr - FB_ADDR)) = data; break;
            case 4: *(uint32_t *)(vmem + (addr - FB_ADDR)) = data; break;
            case 8: *(uint64_t *)(vmem + (addr - FB_ADDR)) = data; break;
        }
    }
    else
    {
        assert(0);
    }
}

void init_device()
{
    init_vga();
}
