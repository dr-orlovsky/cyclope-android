package com.github.macx0r.cyclope;

public final class IonInfo {
	public double mass;
	public double maxCharge;
	public int charge;
	public String name;
	public String briefName;

	public IonInfo(double m, int c, double mc, String n, String sn) {
		this.mass = m;
		this.charge = c;
		this.maxCharge = mc;
		this.name = n;
		this.briefName = sn;
	}
	
	public IonInfo(double m, int c, String n, String sn) {
		this.mass = m;
		this.charge = c;
		this.maxCharge = c;
		this.name = n;
		this.briefName = sn;
	}

	public String toString() {
		return name + " (" + briefName + ")";
	}
	

	static IonInfo[] mainIons = {
		new IonInfo(01.007, 1, "Proton", "H⁺"),
		new IonInfo(06.941, 1, "Lithium", "Li⁺"),
		new IonInfo(22.989769, 1, "Sodium", "Na⁺"),
		new IonInfo(39.0983, 1, "Potassium", "K⁺"),
		new IonInfo(24.305, 2, "Magnesium", "Mg²⁺"),
		new IonInfo(40.078, 2, "Calcium", "Ca²⁺"),	
		new IonInfo(65.38, 2, "Zinc", "Zn²⁺"),
		new IonInfo(63.546, 2, "Copper", "Cu²⁺"),
		new IonInfo(207.2, 2, "Lead ²⁺", "Pb²⁺"),
		new IonInfo(207.2, 4, "Lead ⁴⁺", "Pb⁴⁺"),
	
		new IonInfo(103.12, 1, "GABA", "zwitterion"),
		new IonInfo(147.13, 1, 2, "Glutamate", "zwitterion"),
		new IonInfo(75.0666, 1, 1, "Glycine", "zwitterion"),
		new IonInfo(153.18, 1, 2, "Dopamine", "zwitterion"),
		new IonInfo(169.18, 1, 3, "Norepinephrine", "zwitterion"),
		new IonInfo(176.22, 1, 2, "Serotonin", "zwitterion"),
		new IonInfo(146.21, 1, "Acetylcholine", "zwitterion")
	};
};
