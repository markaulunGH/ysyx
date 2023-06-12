#ifndef __SIM_H__
#define __SIM_H__

#include <config.h>
#include "VTop.h"
#ifdef CONFIG_WAVE_FST
#include "verilated_fst_c.h"
#elif
#include "verilated_vcd_c.h"
#endif
#include <verilated.h>
#include <nvboard.h>

extern const std::unique_ptr<VerilatedContext> contextp;
extern const std::unique_ptr<VTop> top;
#ifdef CONFIG_WAVE_FST
extern VerilatedFstC* tfp;
#elif
extern VerilatedVcdC* tfp;
#endif

void init_simulation(int argc, char** argv);
void end_simulation();
void cycle_end();

#endif