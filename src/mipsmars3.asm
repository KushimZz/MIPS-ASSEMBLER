first:
add $3,$8,$12
sub $5,$10,$15

shifts:
sll $6,$11,3
srl $7,$13,1

immi:
addi $9,$2,100
andi $10,$14,60

branches:
blez $3,immi
bgtz $5,shifts

j first
