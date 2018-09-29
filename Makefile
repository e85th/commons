.PHONY: test clean uberjar

uberjar:
	mkdir target
	clj -A:uberjar

test:
	clj -A:test:compile

clean:
	rm -rf ./target
