start:
add $3,$12,$21
sub $14,$25,$10
and $16,$9,$4
or $22,$5,$30

main:
addi $28,$4,45
andi $24,$19,99
lw $2,25($15)
sw $13,35($18)

branch:
blez $7,main
bgtz $3,label
beq $14,$7,start
bne $16,$9,start

label:
sll $7,$6,5
srl $19,$8,7
sllv $11,$17,$12
srlv $23,$20,$13

j branch