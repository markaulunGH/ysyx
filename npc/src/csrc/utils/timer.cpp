#include <utils.h>
#include <time.h>
#include <sys/time.h>

uint64_t get_time_internal()
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000000 + tv.tv_usec;
}

uint64_t get_time()
{
    static uint64_t boot_time = 0;
    if (boot_time == 0)
    {
        boot_time = get_time_internal();
    }
    return get_time_internal() - boot_time;
}
