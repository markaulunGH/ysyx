#include <log.h>

FILE *log_fp;

void init_log(const char *log_file)
{
    log_fp = fopen(log_file, "w");
}
