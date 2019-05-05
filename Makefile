.PHONY: test clean uberjar

uberjar:
	mkdir target
	clj -A:uberjar

test:
	./bin/kaocha

clean:
	rm -rf ./target
