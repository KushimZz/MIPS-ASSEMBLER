start:
add $2,$5,$8
sub $3,$7,$10
and $4,$9,$11

shifts:
sll $12,$13,4
srl $14,$15,2

main:
addi $16,$1,50
andi $17,$18,120
lw $19,10($20)
sw $21,20($22)

branch:
beq $3,$4,start
bne $5,$6,start

j main
