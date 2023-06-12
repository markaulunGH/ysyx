#ifndef __SIM_H__
#define __SIM_H__

#include "VTop.h"
#include "verilated_fst_c.h"
#include <verilated.h>
#include <nvboard.h>

extern const std::unique_ptr<VerilatedContext> contextp;
extern const std::unique_ptr<VTop> top;
extern VerilatedFstC* tfp;

void init_simulation(int argc, char** argv);
void end_simulation();
void cycle_end();

#endif