package no.nav.jfr.generell.infrastructure.kafka;

import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.operations.SkjemaMetadata;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"no.nav.jfr.generell.Environment"})
@PrepareForTest({SkjemaMetadata.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class KafkaeventFilterTest {

    private SkjemaMetadata skjemaMetadata;
    private static final CompositeConfiguration compositeConfiguration = new CompositeConfiguration();

    @Before
    public void setup() throws Exception {
        spy(Environment.class);
        doReturn("automatiskSkjema.json").when(Environment.class, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING");
        skjemaMetadata = new SkjemaMetadata();

    }

    @Test
    public void test_kanal_eessi(){
        assertFalse(skjemaMetadata.acceptedKanal("MOB", "EESSI"));
        assertFalse(skjemaMetadata.acceptedKanal("GEN", "EESSI"));
        assertFalse(skjemaMetadata.acceptedKanal("AGR", "EESSI"));
        assertFalse(skjemaMetadata.acceptedKanal("SYK", "EESSI"));
    }

    @Test
    public void test_tema_TIL(){
        assertFalse(skjemaMetadata.acceptedKanal("TIL", "EESSI"));
        assertFalse(skjemaMetadata.acceptedKanal("TIL", "ALTINN"));
        assertTrue(skjemaMetadata.acceptedKanal("TIL", "INNSENDT_NAV_ANSATT"));
        assertTrue(skjemaMetadata.acceptedKanal("TIL", "INNSENDT_AV_NAV_ANSATT"));
        assertTrue(skjemaMetadata.acceptedKanal("TIL", "NAV_NO"));
        assertTrue(skjemaMetadata.acceptedKanal("TIL", "SKANN_IM"));
        assertTrue(skjemaMetadata.acceptedKanal("TIL", null));
    }

    @Test
    public void test_tema_SUP(){
        assertFalse(skjemaMetadata.acceptedKanal("SUP", "EESSI"));
        assertFalse(skjemaMetadata.acceptedKanal("SUP", "INNSENDT_NAV_ANSATT"));
        assertFalse(skjemaMetadata.acceptedKanal("SUP", "INNSENDT_AV_NAV_ANSATT"));
        assertTrue(skjemaMetadata.acceptedKanal("SUP", "ALTINN"));
        assertTrue(skjemaMetadata.acceptedKanal("SUP", "NAV_NO"));
        assertTrue(skjemaMetadata.acceptedKanal("SUP", "SKANN_IM"));
        assertTrue(skjemaMetadata.acceptedKanal("SUP", null));
    }

    @Test
    public void test_unsupported_tema(){
        assertFalse(skjemaMetadata.inTemaList("OPP"));
        assertFalse(skjemaMetadata.inTemaList("OPP") && skjemaMetadata.acceptedKanal("UFO", "SKAN_IM"));
        assertFalse(skjemaMetadata.inTemaList("DAG"));
        assertFalse(skjemaMetadata.inTemaList("HEL"));
    }
}
