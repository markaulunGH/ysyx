#include <fs.h>

typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  size_t open_offset;
  ReadFn read;
  WriteFn write;
} Finfo;

enum {FD_STDIN, FD_STDOUT, FD_STDERR, FD_EVENTS, FD_DISPINFO, FD_FB};

extern size_t ramdisk_read(void *buf, size_t offset, size_t len);
extern size_t ramdisk_write(const void *buf, size_t offset, size_t len);
extern size_t serial_write(const void *buf, size_t offset, size_t len);
extern size_t events_read(void *buf, size_t offset, size_t len);
extern size_t dispinfo_read(void *buf, size_t offset, size_t len);
extern size_t fb_write(const void *buf, size_t offset, size_t len);

size_t invalid_read(void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

size_t invalid_write(const void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

/* This is the information about all files in disk. */
static Finfo file_table[] __attribute__((used)) = {
  [FD_STDIN]    = {"stdin", 0, 0, 0, invalid_read, invalid_write},
  [FD_STDOUT]   = {"stdout", 0, 0, 0, invalid_read, serial_write},
  [FD_STDERR]   = {"stderr", 0, 0, 0, invalid_read, serial_write},
  [FD_EVENTS]   = {"/dev/events", 0, 0, 0, events_read, invalid_write},
  [FD_DISPINFO] = {"/proc/dispinfo", 0, 0, 0, dispinfo_read, invalid_write},
  [FD_FB]       = {"/dev/fb", 0, 0, 0, invalid_read, fb_write},
#include "files.h"
};

void init_fs() {
  AM_GPU_CONFIG_T info = io_read(AM_GPU_CONFIG);
  file_table[FD_FB].size = info.width * info.height * 4;
}

int fs_open(const char *pathname, int flags, int mode) {
  for (int i = 0; i < sizeof(file_table) / sizeof(Finfo); ++ i) {
    if (strcmp(pathname, file_table[i].name) == 0) {
      return i;
    }
  }
  panic("File %s not found\n", pathname);
}

size_t fs_read(int fd, void *buf, size_t len) {
  size_t read_len = file_table[fd].size && file_table[fd].open_offset + len > file_table[fd].size ? file_table[fd].size - file_table[fd].open_offset : len;
  read_len = file_table[fd].read ? file_table[fd].read(buf, file_table[fd].disk_offset + file_table[fd].open_offset, read_len) : ramdisk_read(buf, file_table[fd].disk_offset + file_table[fd].open_offset, read_len);
  file_table[fd].open_offset += read_len;
  return read_len;
}

size_t fs_write(int fd, const void *buf, size_t len) {
  size_t write_len = file_table[fd].size && file_table[fd].open_offset + len > file_table[fd].size ? file_table[fd].size - file_table[fd].open_offset : len;
  write_len = file_table[fd].write ? file_table[fd].write(buf, file_table[fd].disk_offset + file_table[fd].open_offset, write_len) : ramdisk_write(buf, file_table[fd].disk_offset + file_table[fd].open_offset, write_len);
  file_table[fd].open_offset += write_len;
  return write_len;
}

size_t fs_lseek(int fd, size_t offset, int whence) {
  switch (whence) {
    case SEEK_SET: file_table[fd].open_offset = offset; break;
    case SEEK_CUR: file_table[fd].open_offset += offset; break;
    case SEEK_END: file_table[fd].open_offset = file_table[fd].size + offset; break;
    default: assert(0);
  }
  return file_table[fd].open_offset;
}

int fs_close(int fd) {
  printf("close %s %d\n", file_table[fd].name, file_table[fd].open_offset);
  file_table[fd].open_offset = 0;
  return 0;
}
