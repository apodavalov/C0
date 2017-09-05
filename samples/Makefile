all: p
p: p.o
	ld -m elf_i386 -s -o p p.o
p.s: p.c0
	./c0.sh p.c0 -o p.s
p.o: p.s
	as --32 -o p.o p.s
clean:
	rm -f p.o p.s p