module top(
    input a,
    input b,
    output f
);

    wire A, B, C;
    assign A = B == 1'b1 ? C : A;
endmodule
