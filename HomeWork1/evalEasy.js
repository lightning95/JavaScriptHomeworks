var cnst = function(value) {
	return function(variable){
		return value;
	};
};

var variable = function(s) {
	return function(variable){
		return variable;
	};
};

var add = function(leftOperand, rightOperand) {
	return function(variable){
		return leftOperand(variable) + rightOperand(variable);
	};
};

var multiply = function(leftOperand, rightOperand) {
	return function(variable){
		return leftOperand(variable) * rightOperand(variable);
	};
};


var subtract = function(leftOperand, rightOperand) {
	return function(variable){
		return leftOperand(variable) - rightOperand(variable);
	};
};

var divide = function(leftOperand, rightOperand) {
	return function(variable){
		return leftOperand(variable) / rightOperand(variable);
	};
};

var expr = add(
				subtract(
					multiply(
						variable('x'), 
						variable('x')
					), 
					multiply(
						variable('x'),
						cnst(2)
					)
				), 
				cnst(1)
			);

for (var i = 0; i < 11; ++i){
	println(expr(i));
}