function nOperation (n, operator){
	return function(){
		var	args = arguments;
		return function(){
			var res = [];
			for (var i = 0; i < n; ++i){
         		res.push(args[i].apply(null, arguments));
			}
			return operator.apply(null, res);
		}
	};
};

var Map = {
	'+'   : {numArg : 2, f : nOperation(2, function(a, b) { return a + b})},
	'-'   : {numArg : 2, f : nOperation(2, function(a, b) { return a - b})},
	'*'   : {numArg : 2, f : nOperation(2, function(a, b) { return a * b})},
	'/'   : {numArg : 2, f : nOperation(2, function(a, b) { return a / b})},
	'log' : {numArg : 1, f : nOperation(1, function(a) { return Math.log(a)})},
	'abs' : {numArg : 1, f : nOperation(1, function(a) { return Math.abs(a)})},
};

var cnst = function(value) {
	return function(){
		return Number(value);
	};
};

var variable = function(s) {
	return function(){
		return (s == 'x') ? arguments[0] : (s == 'y') ? arguments[1] : (s == 'z') ? arguments[2] : null;
	};
};

var isNumber = function(s) {
	return s.match(/\d+/g) != null;
};

var isVariable = function(s) {
	return s.match(/\w+/g) != null;
};
                     
var parse = function(s) {
  	var lexemes = s.match(/\S+/g);
	var stack = [];
	for (var i = 0; i < lexemes.length; ++i){
		if (lexemes[i] in Map){
			var pair = Map[lexemes[i]];
			var kol = pair.numArg;
			var a = [];
			for (var j = 0; j < kol; ++j){
				a.push(stack.pop());
			}
			stack.push(pair.f.apply(null, a.reverse()));
		} else if (isNumber(lexemes[i])) {
			stack.push(cnst(lexemes[i]));
		} else if (isVariable(lexemes[i])){
        	stack.push(variable(lexemes[i]));        			
		}
	}
	return stack.pop();
};  

println(parse('x y z + -')(1, 2, 5));

