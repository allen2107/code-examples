#include <string>
#include <strstream>
#include <fstream>
#include <math.h>
#include <time.h>
#include <vector>
#include <boost/regex.hpp>
using namespace std;

class Pathes {
public:
	string cfxFolder;
	string workFolder;
	string fileName;
	string resultName;
	string solverParameters;

	string mesherFolder;
	string geometryFile;
	string meshFile;
};

class Varible {
protected:
	string name;
	string unit;
public:
	string getName () {
		return name;
	}
	virtual string getNext () = 0;
	virtual double getPreviousValue () = 0;
	virtual void reset () = 0;
	virtual bool end () = 0;
	virtual bool addResult (double res) {return false;}
};

//поиск экстремума на отрезке
class VaribleExtremum : public Varible {
	double v;
	int m;
	double a, b, x1, x2, y1, y2;
	double accuracy;
	double current;
	int pointer;

public:
	VaribleExtremum () {
		v = (sqrt(5.) - 1.) / 2.;
		name = "";
		unit = "";
		a = b = x1 = x2 = y1 = y2 = 0;
		accuracy = 0;
		current = 0;
		pointer = 0;
		m = 0;
	}

	VaribleExtremum (string nm) {
		v = (sqrt(5.) - 1.) / 2.;
		name = nm;
		unit = "";
		a = b = x1 = x2 = y1 = y2 = 0;
		accuracy = 0;
		current = 0;
		pointer = 0;
		m = 0;
	}
	
	VaribleExtremum (string nm, double a1, double b1, int m1, double acc, string u = "") {
		v = (sqrt(5.) - 1.) / 2.;
		name = nm;
		unit = u;
		a = a1;
		b = b1;
		y1 = y2 = 0;
		x1 = a + (1 - v) * (b - a);
		x2 = a + v * (b - a);
		m = m1;
		accuracy = acc;
		current = 0;
		pointer = 0;
	}

	VaribleExtremum (const VaribleExtremum &var) {
		v = (sqrt(5.) - 1.) / 2.;
		name = var.name;
		unit = var.unit;
		a = var.a;
		b = var.b;
		x1 = var.x1;
		x2 = var.x2;
		y1 = var.y1;
		y2 = var.y2;
		accuracy = var.accuracy;
		current = var.current;
		pointer = var.pointer;
		m = var.m;

	}

	string getNext ()
	{
		switch (pointer) {
			case 0: current = x1; break;
			case 1: current = x2; break;
			default:
				if (fabs(x1 - x2) < accuracy) return "";
				if (m * y1 < m * y2) {
					a = x1;
					x1 = x2;
					y1 = y2;
					current = x2 = a + v * (b - a);
				}
				else {
					b = x2;
					x2 = x1;
					y2 = y1;
					current = x1 = a + (1 - v) * (b - a);
				}
		}

		pointer ++;		

		char str[1024];
		strstream out(str, 1024, ios_base::out);
		out << "      " << name << " = " << current << " " << unit << "\0";
		str[out.pcount()] = 0;
		return str;
	}

	bool addResult (double res) {
		if (current == x1)
			y1 = res;
		else 
			y2 = res;

		return (pointer > 1) && fabs(x1 - x2) < accuracy;
	}

	double getPreviousValue () {
		if (pointer == 0)
			return 0;
		else
			return current;
	}

	void reset ()
	{
		pointer = 0;
	}

	bool end () {
		return (pointer > 1) && fabs(x1 - x2) < accuracy;
	}
};

	//
	// SOME CODE HERE
	//

class Varibles {
	VariblesList variblesList;
	string controlVarName;
	int currentNumber;
public:
	Varibles () {currentNumber = 0; controlVarName = "";}
	bool add (char* str) {
		const string numberExpr = "[\\+-]?(?:[[:digit:]]+\\.?|[[:digit:]]*\\.[[:digit:]]+)(?:[EeDd][\\+-]?[[:digit:]]+)?";

		boost::regex a;
		a.set_expression("[[:blank:]]*\\$([[:alnum:]]+)[[:blank:]]*\\[([-\\+[:digit:][:blank:],\\.eEdD]+)\\][[:blank:]]*(\\[.+\\])?(?:[[:blank:]]*\\|[[:blank:]]*(min|max)[[:blank:]]+\\$([[:alnum:]_]+))?[[:blank:]]*");
		boost::smatch xResults;
		string qwerty(str);
		if (boost::regex_match(qwerty, xResults, a)) {
			if (xResults.size() == 6) {
				boost::regex b;
				b.set_expression("(" + numberExpr + ")[[:blank:]]*(" + numberExpr + ")[[:blank:]]*(" + numberExpr + ")");
				boost::smatch xValues;
				string string_xResults_2 = string(xResults[2]);
				boost::regex_match(string_xResults_2, xValues, b);
				if (xValues.size() == 4) {
					if (xResults[5] != "") {
						int m = (xResults[4] == "min" ? -1 : 1);
						VaribleExtremum v(xResults[1], atof(string(xValues[1]).c_str()), atof(string(xValues[2]).c_str()), m, atof(string(xValues[3]).c_str()), xResults[3]); //индекс +1 у всего
						if (variblesList.push_back(v))
							controlVarName = xResults[5];
					}
					else {
						VaribleSet v(xResults[1], atof(string(xValues[1]).c_str()), atof(string(xValues[2]).c_str()), atof(string(xValues[3]).c_str()), xResults[3]);//индекс +1 у всего
						variblesList.push_back(v);
					}
				}
				else
					return false;
			}
			else
				return false;
		}
		else {
			a.set_expression("[[:blank:]]*\\$([[:alnum:]]+)[[:blank:]]*\\{([-\\+[:digit:][:blank:],\\.eEdD]+)\\}[[:blank:]]*(\\[.+\\])?(?:[[:blank:]]*\\|[[:blank:]]*\\$([[:alnum:]_]+)[[:blank:]]*=[[:blank:]]*("+numberExpr+"),[[:blank:]]*("+numberExpr+"))?[[:blank:]]*");
			if (boost::regex_match(qwerty, xResults, a)) {
				if (xResults.size() == 7) {
					boost::regex b;
					b.set_expression("(" + numberExpr + "[[:blank:]]*)");
					string string_xResults_2 = string(xResults[2]);
					vector<string> xValues;
					boost::sregex_token_iterator iter(string_xResults_2.begin(), string_xResults_2.end(), b, 0);
					boost::sregex_token_iterator end;
					for (; iter != end; ++iter) {
						xValues.push_back(*iter);
					}
					int n = xValues.size();
					if (n > 0){
						double* val = new double[n];
						for (int i = 0; i < n; i ++)
							val[i] = atof(string(xValues[i]).c_str());
						if (xResults[4] != "" && xResults[5] != "" && xResults[6] != "" && n == 2) {
							VaribleAt v(xResults[1], val, atof(string(xResults[5]).c_str()), atof(string(xResults[6]).c_str()), xResults[3]);
							if (variblesList.push_back(v))
								controlVarName = xResults[4];
						}
						else {
							VaribleSet v(xResults[1], n, val, xResults[3]);
							variblesList.push_back(v);
						}
					}
					else
						return false;
				}
				else
					return false;
			}
			else
				return false;
		}

		return true;
	}
	
	//
	// SOME CODE HERE
	//
};
