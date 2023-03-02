// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VGCD.h for the primary calling header

#ifndef VERILATED_VGCD___024ROOT_H_
#define VERILATED_VGCD___024ROOT_H_  // guard

#include "verilated_heavy.h"

//==========

class VGCD__Syms;
class VGCD_VerilatedFst;


//----------

VL_MODULE(VGCD___024root) {
  public:

    // PORTS
    VL_IN8(clock,0,0);
    VL_IN8(reset,0,0);
    VL_IN8(io_loadingValues,0,0);
    VL_OUT8(io_outputValid,0,0);
    VL_IN16(io_value1,15,0);
    VL_IN16(io_value2,15,0);
    VL_OUT16(io_outputGCD,15,0);

    // LOCAL SIGNALS
    SData/*15:0*/ GCD__DOT__x;
    SData/*15:0*/ GCD__DOT__y;

    // LOCAL VARIABLES
    CData/*0:0*/ __Vclklast__TOP__clock;

    // INTERNAL VARIABLES
    VGCD__Syms* vlSymsp;  // Symbol table

    // CONSTRUCTORS
  private:
    VL_UNCOPYABLE(VGCD___024root);  ///< Copying not allowed
  public:
    VGCD___024root(const char* name);
    ~VGCD___024root();

    // INTERNAL METHODS
    void __Vconfigure(VGCD__Syms* symsp, bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);

//----------


#endif  // guard
