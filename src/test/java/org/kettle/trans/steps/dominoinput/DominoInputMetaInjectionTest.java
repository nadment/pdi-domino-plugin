package org.kettle.trans.steps.dominoinput;

import org.junit.Before;
import org.junit.Test;
import org.kettle.trans.steps.dominoinput.DominoInputMeta;
import org.pentaho.di.core.injection.BaseMetadataInjectionTest;

public class DominoInputMetaInjectionTest extends BaseMetadataInjectionTest<DominoInputMeta> {
	@Before
	public void setup() {
		setup(new DominoInputMeta());
	}

	@Test
	public void test() throws Exception {

		check("VIEW", new StringGetter() {
			@Override
			public String get() {
				return meta.getView();
			}
		});

		check("SEARCH", new StringGetter() {
			@Override
			public String get() {
				return meta.getSearch();
			}
		});
		
		skipPropertyTest( "CONNECTIONNAME" );
	}
}
