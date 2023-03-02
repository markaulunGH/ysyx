// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Tracing implementation internals
#include "verilated_fst_c.h"
#include "VGCD__Syms.h"


void VGCD___024root__traceInitSub0(VGCD___024root* vlSelf, VerilatedFst* tracep) VL_ATTR_COLD;

void VGCD___024root__traceInitTop(VGCD___024root* vlSelf, VerilatedFst* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    {
        VGCD___024root__traceInitSub0(vlSelf, tracep);
    }
}

void VGCD___024root__traceInitSub0(VGCD___024root* vlSelf, VerilatedFst* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    const int c = vlSymsp->__Vm_baseCode;
    if (false && tracep && c) {}  // Prevent unused
    // Body
    {
        tracep->declBit(c+1,"clock",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBit(c+2,"reset",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBus(c+3,"io_value1",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1, 15,0);
        tracep->declBus(c+4,"io_value2",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1, 15,0);
        tracep->declBit(c+5,"io_loadingValues",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBus(c+6,"io_outputGCD",-1,FST_VD_OUTPUT,FST_VT_VCD_WIRE, false,-1, 15,0);
        tracep->declBit(c+7,"io_outputValid",-1,FST_VD_OUTPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBit(c+1,"GCD clock",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBit(c+2,"GCD reset",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBus(c+3,"GCD io_value1",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1, 15,0);
        tracep->declBus(c+4,"GCD io_value2",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1, 15,0);
        tracep->declBit(c+5,"GCD io_loadingValues",-1,FST_VD_INPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBus(c+6,"GCD io_outputGCD",-1,FST_VD_OUTPUT,FST_VT_VCD_WIRE, false,-1, 15,0);
        tracep->declBit(c+7,"GCD io_outputValid",-1,FST_VD_OUTPUT,FST_VT_VCD_WIRE, false,-1);
        tracep->declBus(c+8,"GCD x",-1, FST_VD_IMPLICIT,FST_VT_SV_LOGIC, false,-1, 15,0);
        tracep->declBus(c+9,"GCD y",-1, FST_VD_IMPLICIT,FST_VT_SV_LOGIC, false,-1, 15,0);
    }
}

void VGCD___024root__traceFullTop0(void* voidSelf, VerilatedFst* tracep) VL_ATTR_COLD;
void VGCD___024root__traceChgTop0(void* voidSelf, VerilatedFst* tracep);
void VGCD___024root__traceCleanup(void* voidSelf, VerilatedFst* /*unused*/);

void VGCD___024root__traceRegister(VGCD___024root* vlSelf, VerilatedFst* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    {
        tracep->addFullCb(&VGCD___024root__traceFullTop0, vlSelf);
        tracep->addChgCb(&VGCD___024root__traceChgTop0, vlSelf);
        tracep->addCleanupCb(&VGCD___024root__traceCleanup, vlSelf);
    }
}

void VGCD___024root__traceFullSub0(VGCD___024root* vlSelf, VerilatedFst* tracep) VL_ATTR_COLD;

void VGCD___024root__traceFullTop0(void* voidSelf, VerilatedFst* tracep) {
    VGCD___024root* const __restrict vlSelf = static_cast<VGCD___024root*>(voidSelf);
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    {
        VGCD___024root__traceFullSub0((&vlSymsp->TOP), tracep);
    }
}

void VGCD___024root__traceFullSub0(VGCD___024root* vlSelf, VerilatedFst* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    VGCD__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    vluint32_t* const oldp = tracep->oldp(vlSymsp->__Vm_baseCode);
    if (false && oldp) {}  // Prevent unused
    // Body
    {
        tracep->fullBit(oldp+1,(vlSelf->clock));
        tracep->fullBit(oldp+2,(vlSelf->reset));
        tracep->fullSData(oldp+3,(vlSelf->io_value1),16);
        tracep->fullSData(oldp+4,(vlSelf->io_value2),16);
        tracep->fullBit(oldp+5,(vlSelf->io_loadingValues));
        tracep->fullSData(oldp+6,(vlSelf->io_outputGCD),16);
        tracep->fullBit(oldp+7,(vlSelf->io_outputValid));
        tracep->fullSData(oldp+8,(vlSelf->GCD__DOT__x),16);
        tracep->fullSData(oldp+9,(vlSelf->GCD__DOT__y),16);
    }
}
