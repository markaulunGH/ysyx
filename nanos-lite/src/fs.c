#include <fs.h>

typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  ReadFn read;
  WriteFn write;
} Finfo;

enum {FD_STDIN, FD_STDOUT, FD_STDERR, FD_EVENTS, FD_FB};

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
  [FD_STDIN]  = {"stdin", 0, 0, invalid_read, invalid_write},
  [FD_STDOUT] = {"stdout", 0, 0, invalid_read, serial_write},
  [FD_STDERR] = {"stderr", 0, 0, invalid_read, serial_write},
  [FD_EVENTS] = {"/dev/events", 0, 0, events_read, invalid_write},
#include "files.h"
};

void init_fs() {
  // TODO: initialize the size of /dev/fb
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
  size_t read_size = file_table[fd].read ? file_table[fd].read(buf, file_table[fd].disk_offset, len) : ramdisk_read(buf, file_table[fd].disk_offset, len);
  file_table[fd].disk_offset += read_size;
  return read_size;
}

size_t fs_write(int fd, const void *buf, size_t len) {
  size_t write_size = file_table[fd].write ? file_table[fd].write(buf, file_table[fd].disk_offset, len) : ramdisk_write(buf, file_table[fd].disk_offset, len);
  file_table[fd].disk_offset += write_size;
  return write_size;
}

size_t fs_lseek(int fd, size_t offset, int whence) {
  size_t disk_start = 0;
  for (int i = 0; i < fd; ++ i) {
    disk_start += file_table[i].size;
  }
  switch (whence) {
    case SEEK_SET: file_table[fd].disk_offset = disk_start + offset; break;
    case SEEK_CUR: file_table[fd].disk_offset += offset; break;
    case SEEK_END: file_table[fd].disk_offset = disk_start + file_table[fd].size + offset; break;
    default: assert(0);
  }
  return file_table[fd].disk_offset - disk_start;
}

int fs_close(int fd) {
  return 0;
}
