
//implicit "eval" symbol

var a;
b = 1;

function f (p1) {
 // implicit "arguments" symbol
  var a;
  c = 1;  // implicit declaration - global scope
}

try {

} catch (e) {
  let a;
}

f((p2) => {return a+1}) // implicit "arguments" symbol

for (i of a){}
for (i in a){}
