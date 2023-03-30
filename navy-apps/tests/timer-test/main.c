#include <stdio.h>
#include <NDL.h>

int main()
{
  NDL_Init(0);
  int halfsec = 1;
  while (1) {
    while(NDL_GetTicks() / 500 < halfsec);
    printf("%d\n", halfsec);
    halfsec ++;
  }
}