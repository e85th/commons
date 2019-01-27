.PHONY: test clean uberjar

uberjar:
	mkdir target
	clj -A:uberjar

test:
	clj -A:compile:test

clean:
	rm -rf ./target
