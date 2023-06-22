#ifndef __SIM_H__
#define __SIM_H__

#include <config.h>
#include "VTop.h"
#ifdef CONFIG_WAVE_FST
#include "verilated_fst_c.h"
#else
#include "verilated_vcd_c.h"
#endif
#include <verilated.h>
#include <nvboard.h>

extern const std::unique_ptr<VerilatedContext> contextp;
extern const std::unique_ptr<VTop> top;
#ifdef CONFIG_WAVE_FST
extern VerilatedFstC* tfp;
#else
extern VerilatedVcdC* tfp;
#endif
extern bool wave_enable;

void init_simulation(int argc, char** argv);
void end_simulation();
void cycle_end();

#endif