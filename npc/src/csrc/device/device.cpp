#include <device.h>
#include <utils.h>
#include <SDL2/SDL.h>
#include <sdb.h>

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

#define KEYDOWN_MASK 0x8000

#define _KEYS(f) \
  f(ESCAPE) f(F1) f(F2) f(F3) f(F4) f(F5) f(F6) f(F7) f(F8) f(F9) f(F10) f(F11) f(F12) \
f(GRAVE) f(1) f(2) f(3) f(4) f(5) f(6) f(7) f(8) f(9) f(0) f(MINUS) f(EQUALS) f(BACKSPACE) \
f(TAB) f(Q) f(W) f(E) f(R) f(T) f(Y) f(U) f(I) f(O) f(P) f(LEFTBRACKET) f(RIGHTBRACKET) f(BACKSLASH) \
f(CAPSLOCK) f(A) f(S) f(D) f(F) f(G) f(H) f(J) f(K) f(L) f(SEMICOLON) f(APOSTROPHE) f(RETURN) \
f(LSHIFT) f(Z) f(X) f(C) f(V) f(B) f(N) f(M) f(COMMA) f(PERIOD) f(SLASH) f(RSHIFT) \
f(LCTRL) f(APPLICATION) f(LALT) f(SPACE) f(RALT) f(RCTRL) \
f(UP) f(DOWN) f(LEFT) f(RIGHT) f(INSERT) f(DELETE) f(HOME) f(END) f(PAGEUP) f(PAGEDOWN)

#define _KEY_NAME(k) _KEY_##k,

#define concat_temp(x, y) x ## y
#define concat(x, y) concat_temp(x, y)
#define MAP(c, f) c(f)

enum
{
    _KEY_NONE = 0,
    MAP(_KEYS, _KEY_NAME)
};

#define SDL_KEYMAP(k) keymap[concat(SDL_SCANCODE_, k)] = concat(_KEY_, k);
uint32_t keymap[256] = {};

uint32_t i8042_data_port_base[1];

void init_i8042()
{
    i8042_data_port_base[0] = _KEY_NONE;
    MAP(_KEYS, SDL_KEYMAP)
}

#define KEY_QUEUE_LEN 1024
int key_queue[KEY_QUEUE_LEN] = {};
int key_f = 0, key_r = 0;

void key_enqueue(uint32_t am_scancode)
{
    key_queue[key_r] = am_scancode;
    key_r = (key_r + 1) % KEY_QUEUE_LEN;
}

uint32_t key_dequeue()
{
    uint32_t key = _KEY_NONE;
    if (key_f != key_r)
    {
        key = key_queue[key_f];
        key_f = (key_f + 1) % KEY_QUEUE_LEN;
    }
    return key;
}

void send_key(uint8_t scancode, bool is_keydown)
{
    if (npc_state.state == NPC_RUNNING && keymap[scancode] != _KEY_NONE)
    {
        uint32_t am_scancode = keymap[scancode] | (is_keydown ? KEYDOWN_MASK : 0);
        key_enqueue(am_scancode);
    }
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
    else if (addr == KBD_ADDR)
    {
        SDL_Event event;
        while (SDL_PollEvent(&event))
        {
            switch (event.type)
            {
                // If a key was pressed
                case SDL_KEYDOWN:
                case SDL_KEYUP:
                {
                    uint8_t k = event.key.keysym.scancode;
                    bool is_keydown = (event.key.type == SDL_KEYDOWN);
                    send_key(k, is_keydown);
                    break;
                }
                default: break;
            }
        }
        i8042_data_port_base[0] = key_dequeue();
        return i8042_data_port_base[0];
    }
    else
    {
        printf("mmio_read: addr = %lx, len = %d\n", addr, len);
        npc_state.state = NPC_ABORT;
        return 0;
    }
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
        printf("mmio_write: addr = %lx, len = %d, data = %lx\n", addr, len, data);
        npc_state.state = NPC_ABORT;
    }
}

void init_device()
{
    init_vga();
    init_i8042();
}
