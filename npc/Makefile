BUILD_DIR = ./build

export PATH := $(PATH):$(abspath ./utils)
export PATH := $(PATH):$(abspath ./)

ifeq ($(VERILATOR_ROOT),)
VERILATOR = verilator
VERILATOR_COVERAGE = verilator_coverage
else
export VERILATOR_ROOT
VERILATOR = $(VERILATOR_ROOT)/bin/verilator
VERILATOR_COVERAGE = $(VERILATOR_ROOT)/bin/verilator_coverage
endif

CSRC_DIR = $(abspath src/csrc)
CSRC += $(shell find $(CSRC_DIR) -name "*.cpp")
CFLAGS += $(addprefix -I, $(abspath src/csrc/include))
CFLAGS += -g
LDFLAGS += -lreadline

CSRC += $(CSRC_DIR)/utils/disasm.cc
CFLAGS += $(shell llvm-config --cxxflags) -fPIE
LDFLAGS += $(shell llvm-config --libs)

include $(NVBOARD_HOME)/scripts/nvboard.mk
INCFLAGS += $(addprefix -I, $(INC_PATH))
CFLAGS += $(INCFLAGS)
LDFLAGS += -lSDL2 -lSDL2_image

VERILATOR_FLAGS += -cc --exe -O2 -x-assign fast -Wall -Wno-UNUSED -Wno-DECLFILENAME --trace --assert --build
# VERILATOR_FLAGS += --coverage
# VERILATOR_FLAGS += --debug
# VERILATOR_FLAGS += --gdbbt

VERILATOR_INPUT = build/Top.v $(CSRC) $(NVBOARD_ARCHIVE)

.DEFAULT_GOAL := verilog

test:
	mill -i __.test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i __.test.runMain Elaborate -td $(BUILD_DIR)

help:
	mill -i __.test.runMain Elaborate --help

compile:
	mill -i __.compile

bsp:
	mill -i mill.bsp.BSP/install

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

.PHONY: test verilog help compile bsp reformat checkformat clean

IMG ?=
ELF ?=
SO ?=
ARGS ?=

sim:
	mkdir -p $(BUILD_DIR)
	mill -i __.test.runMain Elaborate -td $(BUILD_DIR)
	
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!

	@$^
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_INPUT) $(addprefix -CFLAGS , $(CFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS))

	@rm -rf logs
	@mkdir -p logs
	obj_dir/VTop +trace --img=$(IMG) --elf=$(ELF) --log=$(abspath logs/log.txt) --diff=$(SO) $(ARGS)

gdb:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!

	@$^
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_INPUT) $(addprefix -CFLAGS , $(CFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS))

	@rm -rf logs
	@mkdir -p logs
	gdb --args obj_dir/VTop +trace --img=$(IMG) --elf=$(ELF) --log=$(abspath logs/log.txt) --diff=$(SO) ARGS

maintainer-copy::
clean mostlyclean distclean maintainer-clean::
	-rm -rf obj_dir logs *.log *.dmp *.vpd coverage.dat core $(BUILD_DIR)

include ../Makefile
