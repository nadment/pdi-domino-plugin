package org.pentaho.di.trans.steps.dominoinput;

import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.row.value.ValueMetaString;

/**
 * Contains the properties of the inputs fields, target field name, target value
 * type and options.
 *
 * @author Nicolas ADMENT
 */
public class DominoField implements Cloneable {

	public DominoField() {
		super();

		this.type = ValueMetaInterface.TYPE_STRING;
		this.format = "";
		this.trimtype = ValueMetaBase.TRIM_TYPE_NONE;
		this.groupSymbol = "";
		this.decimalSymbol = "";
		this.currencySymbol = "";
		this.precision = -1;
	}

	/** The target field name */
	@Injection(name = "NAME", group = "FIELDS")
	private String name;

	private int type;
	@Injection(name = "FORMULA", group = "FIELDS")
	private String formula;

	@Injection(name = "LENGTH", group = "FIELDS")
	private int length = -1;
	@Injection(name = "PRECISION", group = "FIELDS")
	private int precision = -1;
	private int trimtype;
	@Injection(name = "FORMAT", group = "FIELDS")
	private String format;
	@Injection(name = "CURRENCY", group = "FIELDS")
	private String currencySymbol;
	@Injection(name = "DECIMAL", group = "FIELDS")
	private String decimalSymbol;
	@Injection(name = "GROUP", group = "FIELDS")
	private String groupSymbol;


	
	@Override
	public Object clone() {
		DominoField clone;
		try {
			clone = (DominoField) super.clone();
			// clone.inputFields = Arrays.copyOf(inputFields, noInputFields);
		} catch (CloneNotSupportedException e) {
			return null;
		}
		return clone;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	private String getTypeDesc() {
		return ValueMetaFactory.getValueMetaName(type);
	}

	@Injection(name = "TYPE", group = "FIELDS")
	public void setType(final String name) {
		this.type = ValueMetaFactory.getIdForValueMeta(name);
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}
	
	public int getTrimType() {
		return trimtype;
	}

	public String getTrimTypeCode() {
		return ValueMetaString.getTrimTypeCode(trimtype);
	}
		
	
	public String getTrimTypeDesc() {
		return ValueMetaString.getTrimTypeDesc(trimtype);
	}
			
	public void setTrimType(int trimtype) {
		this.trimtype = trimtype;
	}

	@Injection(name = "TRIM_TYPE", group = "FIELDS")
	public void setTrimTypeCode(final String trimType) {
		this.trimtype = ValueMetaBase.getTrimTypeByCode(trimType);
	}

	public void setTrimTypeDesc(final String trimType) {
		this.trimtype = ValueMetaString.getTrimTypeByDesc(trimType);
	}

	public String getGroupSymbol() {
		return groupSymbol;
	}

	public void setGroupSymbol(String group_symbol) {
		this.groupSymbol = group_symbol;
	}

	public String getDecimalSymbol() {
		return decimalSymbol;
	}

	public void setDecimalSymbol(String decimal_symbol) {
		this.decimalSymbol = decimal_symbol;
	}

	public String getCurrencySymbol() {
		return currencySymbol;
	}

	public void setCurrencySymbol(String currency_symbol) {
		this.currencySymbol = currency_symbol;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}



	@Override
	public String toString() {

		return name + ":" + getTypeDesc() + "(" + length + "," + precision + ")";
	}
}
