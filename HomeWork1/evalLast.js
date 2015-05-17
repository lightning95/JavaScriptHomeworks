var cnst = function(value) {
	return function(){
		return Number(value);
	};
};

var variable = function(s) {
	return function(x, y, z){
			return (s == 'x') ? x : (s == 'y') ? y : z;
	};
};

var binaryOperation = function (operator) {
	return function(leftOperand, rightOperand) {
		return function(x, y, z){
			return operator(leftOperand(x, y, z), rightOperand(x, y, z));		
		};
	};
};

var unaryOperation = function (operator){
	return function(operand){
		return function(x, y, z){
			return operator(operand(x, y, z));
		};
	};
};

var add      = binaryOperation(function (a, b){
			return a + b;    	                                
	}
);

var log = unaryOperation(Math.log);

var subtract = binaryOperation(function (a, b) {
		return a - b;
	}
);

var multiply = binaryOperation(function (a, b) {
		return a * b;
	}
);

var divide   = binaryOperation(function (a, b) {
		return a / b;
	}
);

var abs = unaryOperation(Math.abs);
var pi = cnst(Math.PI);

var isNumber = function(s) {
	return s.match(/\d+/g) != null;
};

var isVariable = function(s) {
	return s.match(/\w+/g) != null;
};
                     

var parse = function(s) {
  	var lexemes = s.match(/\d+|(pi)|(log)|(abs)|[-]|[xyz+/*]|\w/g);
	var stack = [];
	for (var i = 0; i < lexemes.length; ++i){
		if (lexemes[i] == 'pi'){
			stack.push(pi);			
		} else
		if (lexemes[i] == 'abs'){
			stack.push(abs(stack.pop()));
		} else if (lexemes[i] == 'log'){
			stack.push(log(stack.pop()));
		} else if (lexemes[i] == '+'){
			stack.push(add(stack.pop(), stack.pop()));
		} else if (lexemes[i] == '-'){
			var cur = stack.pop();
			stack.push(subtract(stack.pop(), cur));
		} else if (lexemes[i] == '*'){
			stack.push(multiply(stack.pop(), stack.pop()));
		} else if (lexemes[i] == '/'){
        	var cur = stack.pop();
			stack.push(divide(stack.pop(), cur));
		} else if (isNumber(lexemes[i])) {
			stack.push(cnst(lexemes[i]));
		} else if (isVariable(lexemes[i])){
        	stack.push(variable(lexemes[i]));        			
		}
	}
	return stack.pop();
};  

println(parse('x y + z *')(1, 2, 3));

