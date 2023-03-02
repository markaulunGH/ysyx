// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VGCD.h for the primary calling header

#include "VGCD___024root.h"
#include "VGCD__Syms.h"

//==========

VL_INLINE_OPT void VGCD___024root___sequent__TOP__1(VGCD___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VGCD___024root___sequent__TOP__1\n"); );
    // Variables
    SData/*15:0*/ __Vdly__GCD__DOT__x;
    SData/*15:0*/ __Vdly__GCD__DOT__y;
    // Body
    __Vdly__GCD__DOT__y = vlSelf->GCD__DOT__y;
    __Vdly__GCD__DOT__x = vlSelf->GCD__DOT__x;
    if (vlSelf->io_loadingValues) {
        __Vdly__GCD__DOT__x = vlSelf->io_value1;
        __Vdly__GCD__DOT__y = vlSelf->io_value2;
    } else if (((IData)(vlSelf->GCD__DOT__x) > (IData)(vlSelf->GCD__DOT__y))) {
        __Vdly__GCD__DOT__x = (0xffffU & ((IData)(vlSelf->GCD__DOT__x) 
                                          - (IData)(vlSelf->GCD__DOT__y)));
    } else {
        __Vdly__GCD__DOT__y = (0xffffU & ((IData)(vlSelf->GCD__DOT__y) 
                                          - (IData)(vlSelf->GCD__DOT__x)));
    }
    vlSelf->GCD__DOT__x = __Vdly__GCD__DOT__x;
    vlSelf->GCD__DOT__y = __Vdly__GCD__DOT__y;
    vlSelf->io_outputGCD = vlSelf->GCD__DOT__x;
    vlSelf->io_outputValid = (0U == (IData)(vlSelf->GCD__DOT__y));
}

void VGCD___024root___eval(VGCD___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VGCD___024root___eval\n"); );
    // Body
    if (((IData)(vlSelf->clock) & (~ (IData)(vlSelf->__Vclklast__TOP__clock)))) {
        VGCD___024root___sequent__TOP__1(vlSelf);
    }
    // Final
    vlSelf->__Vclklast__TOP__clock = vlSelf->clock;
}

QData VGCD___024root___change_request_1(VGCD___024root* vlSelf);

VL_INLINE_OPT QData VGCD___024root___change_request(VGCD___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VGCD___024root___change_request\n"); );
    // Body
    return (VGCD___024root___change_request_1(vlSelf));
}

VL_INLINE_OPT QData VGCD___024root___change_request_1(VGCD___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VGCD___024root___change_request_1\n"); );
    // Body
    // Change detection
    QData __req = false;  // Logically a bool
    return __req;
}

#ifdef VL_DEBUG
void VGCD___024root___eval_debug_assertions(VGCD___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VGCD___024root___eval_debug_assertions\n"); );
    // Body
    if (VL_UNLIKELY((vlSelf->clock & 0xfeU))) {
        Verilated::overWidthError("clock");}
    if (VL_UNLIKELY((vlSelf->reset & 0xfeU))) {
        Verilated::overWidthError("reset");}
    if (VL_UNLIKELY((vlSelf->io_loadingValues & 0xfeU))) {
        Verilated::overWidthError("io_loadingValues");}
}
#endif  // VL_DEBUG
