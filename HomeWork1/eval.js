
function nOperation (operator){
	return function(){
		var	args = arguments;
		return function(){
			var res = [];
			for (var i = 0; i < args.length; ++i){
         		res.push(args[i].apply(null, arguments));
			}
			return operator.apply(null, res);
		}
	};
};

function binaryOperation (operator) {
	return function(leftOperand, rightOperand) {
		return function(){
			return operator(leftOperand.apply(null, arguments), rightOperand.apply(null, arguments));		
		};
	};
};

var log = nOperation(Math.log);
var abs = nOperation(Math.abs);

var add      = binaryOperation(function (a, b){	return a + b; });

var subtract = binaryOperation(function (a, b){	return a - b; });

var multiply = binaryOperation(function (a, b){	return a * b; });

var divide   = binaryOperation(function (a, b){ return a / b; });

var Map = {
	'+'   : {numArg : 2, f : add},
	'-'   : {numArg : 2, f : subtract},
	'*'   : {numArg : 2, f : multiply},
	'/'   : {numArg : 2, f : divide},
	'log' : {numArg : 1, f : log},
	'abs' : {numArg : 1, f : abs},
	'cos' : {numArg : 1, f : function(operand){ return function(){ return Math.cos(operand.apply(null, arguments)); }; }  },
	'trio'  : {numArg : 3, f : nOperation(function(a, b, c){  return a + b + c;} )}
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

println(parse('x y z trio')(1, 2, 5));

