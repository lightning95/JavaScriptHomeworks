var Map = {
	'+'   : {numArg : 2, f : Add = new FunMaker(function(a, b){ return a + b; }, '+', 
			//  diff
			function(x){
				return new Add(this.args[0].diff(x), this.args[1].diff(x));
			} 
			)},
	'-'   : {numArg : 2, f : Subtract = new FunMaker(function(a, b) { return a - b; }, '-', 
			//  diff
			function(x){
				return new Subtract(this.args[0].diff(x), this.args[1].diff(x));
			}
			/*function() {
				var l = this.args[0].simplify();
				var r = this.args[1].simplify();
				if (l instanceof Const && r instanceof Const && isNaN(l.value - r.value)){
					return new Const(NaN);
				}
				if (l instanceof Const && r instanceof Const
					 /*&& !isNaN(l.value - r.value) && l.value - r.value >= 0
					 ){
					return new Const(l.value - r.value);
				}
				if (r instanceof Const && r.value == 0){
					return l;
				}
				/*if (l instanceof Variable && r instanceof Variable && l.name == r.name){
				  	return new Const(0);
				} 
				return new Subtract(l, r);
			}*/
			)},
	'*'   : {numArg : 2, f : Multiply = new FunMaker(function(a, b) { return a * b; }, '*', 
			function(x){
				return new Add(
					new Multiply(this.args[0].diff(x), this.args[1]), 
					new Multiply(this.args[0], this.args[1].diff(x)));
			}
			/*function() {
				var l = this.args[0].simplify();
				var r = this.args[1].simplify();
				if (l instanceof Const && r instanceof Const && isNaN(l.value * r.value)){
					return new Const(NaN);
				}

				if (l instanceof Const && r instanceof Const
					 /*&& !isNaN(l.value * r.value)
					  && l.value * r.value >= 0
					 ){
					return new Const(l.value * r.value);
				}
				/*if (/*!isNaN(l.value * r.value) && (l instanceof Const && l.value == 0 || r instanceof Const && r.value == 0)){
					return new Const(0);
				}
				if (l instanceof Const && l.value == 1){
					return r;
				}
				if (r instanceof Const && r.value == 1){
					return l;
				}
				return new Multiply(l, r);
			}*/)},
	'/'   : {numArg : 2, f : Divide = new FunMaker(function(a, b) { return a / b; }, '/', 
			function(x){
				return new Divide(
					new Subtract(
						new Multiply(this.args[0].diff(x), this.args[1]), 
						new Multiply(this.args[0], this.args[1].diff(x))), 
					new Multiply(this.args[1], this.args[1]));
			}
			/*function() {
				var l = this.args[0].simplify();
				var r = this.args[1].simplify();
				if (l instanceof Const && r instanceof Const && isNaN(l.value / r.value)){
					return new Const(NaN);
				}

				if (l instanceof Const && r instanceof Const
					 /*&& !isNaN(l.value / r.value) && l.value / r.value >= 0
					 ){
					return new Const(l.value / r.value);
				}
				/*if (/*!isNaN(r.value) && l instanceof Const && l.value == 0){
					return new Const(0);
				}                         
				if (r instanceof Const && r.value == 1){
					return l;
				}
				/*if (l instanceof Variable && r instanceof Variable && l.name == r.name){
					return new Const(1);
				}
				return new Divide(l, r);
			}*/)},
	'cos' : {numArg : 1, f : Cos = new FunMaker(Math.cos, 'cos', 
			function(x) {
				return new Subtract(Const.prototype.ZERO, new Multiply(new Sin(this.args[0]), this.args[0].diff(x)));
			}
			/*function(){
			 	var v = this.args[0].simplify();
			 	if (v instanceof Const
					 && !isNaN(Math.cos(v.value)) && Math.cos(v.value) >= 0
				){
                	return new Const(Math.cos(v.value));
			 	}
			 	return new Cos(v);
			}*/)},
	'sin' : {numArg : 1, f : Sin = new FunMaker(Math.sin, 'sin', 
			function(x) {
				return new Multiply(new Cos(this.args[0]), this.args[0].diff(x));
			}
			/*function(){
			 	var v = this.args[0].simplify();
			 	if (v instanceof Const
					 && !isNaN(Math.sin(v.value)) && Math.sin(v.value) >= 0
				){
                	return new Const(Math.sin(v.value));
			 	}
			 	return new Sin(v);
			}*/)},
};

function Variable(name) {
	this.name = name;
}
{
	Variable.prototype.evaluate = function(){
		return this.name == 'x' ? arguments[0] : 
			   this.name == 'y' ? arguments[1] : 
			   this.name == 'z' ? arguments[2] : 
			   null;
	};	
	Variable.prototype.diff = function(x){
		return this.name == x ? Const.prototype.ONE : Const.prototype.ZERO;
	};
	Variable.prototype.toString = function(){
		return this.name;
	};
	Variable.prototype.simplify = function(){
		return this;
	};
}


function Const(value){
	this.value = Number(value);
}
{
	Const.prototype.ONE = new Const(1);
	Const.prototype.ZERO = new Const(0);
	Const.prototype.evaluate = function() {
		return this.value;
	};
	Const.prototype.diff = function(){
		return Const.prototype.ZERO;
	};
	Const.prototype.toString = function(){
    	return this.value;
	};
	Const.prototype.simplify = function() {
		return this;
	};
}

function FunMaker(op, sop, diff){
	var f = function(){
	    this.args = arguments;
	    return this;
	}
	f.prototype.op = op;
	f.prototype.sop = sop;
	f.prototype.evaluate = function(){
		var res = [];
		for (var i = 0; i < this.args.length; ++i){
			res.push(this.args[i].evaluate.apply(this.args[i], arguments));
		}
		return this.op.apply(this.op, res);
	};
	f.prototype.toString = function(){
		var res = '';
		for (var i = 0; i < this.args.length; ++i){
			res += this.args[i].toString() + ' ';
		}
		return res + this.sop + ' ';        
	};

	f.prototype.simplify = function() {
		var kol = 0;
		for (var i = 0; i < this.args.length; ++i){
			this.args[i] = this.args[i].simplify();
			if (this.args[i] instanceof Const){
				++kol;
			}
		}
  		if (kol == this.args.length){
	  		var cur = this.op.apply(this.op, this.args);
  			if (cur.value >= 0){
	  			return new Const(cur);
  			}
  		}
  		return this;
   	};
   	f.prototype.diffArgs = function(x){
   		var args = [];
		for (var i = 0; i < this.args.length; ++i){
			args.push(this.args[i].diff(x));
		}
		return args;
   	};

	f.prototype.diff = function(x){
		return diff.call(this, x).simplify();
	};
	return f;
};

var isNumber = function(s) {
	return s.match(/\d+/g) != null;
};

var isVariable = function(s) {
	return s.match(/\w+/g) != null;
};

var Cos = Map['cos'].f;
var Sin = Map['sin'].f;
var Add = Map['+'].f;
var Subtract = Map['-'].f;
var Multiply = Map['*'].f;
var Divide = Map['/'].f;

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
			stack.push(pair.f.apply(Object.create(pair.f.prototype), a.reverse()));
		} else if (isNumber(lexemes[i])) {
			stack.push(new Const(lexemes[i]));
		} else if (isVariable(lexemes[i])){
        	stack.push(new Variable(lexemes[i]));        			
		}
		stack.push(stack.pop());
	}
	return stack.pop().simplify();
};  

//var println = console.log;
//println(parse('x').diff('x').evaluate(1));
//println(parse('x 2 2 3 + - - cos').simplify().toString());
//var e = parse(" 2 x * 3 - ");
//println(e.diff('x').toString());
