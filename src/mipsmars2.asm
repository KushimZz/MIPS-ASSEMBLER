init:
add $2,$5,$8
sub $3,$7,$10
and $4,$9,$11
or $6,$12,$13

shift_ops:
sll $14,$15,4
srl $16,$17,5
sllv $18,$19,$20
srlv $21,$22,$23

operations:
addi $24,$1,100
andi $25,$26,120
lw $27,50($28)
sw $29,60($30)

conditional:
blez $2,operations
bgtz $3,shift_ops
beq $4,$5,init
bne $6,$7,init

jump_label:
j conditional
