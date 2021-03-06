package com.quorum.tessera.q2t;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jpmorgan.quorum.mock.servicelocator.MockServiceLocator;
import com.quorum.tessera.config.AppType;
import com.quorum.tessera.config.ClientMode;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.service.locator.ServiceLocator;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Q2TRestAppBesuTest {

  private JerseyTest jersey;

  private Q2TRestApp q2TRestApp;

  @Before
  public void setUp() throws Exception {
    Config config = mock(Config.class);
    when(config.getClientMode()).thenReturn(ClientMode.ORION);
    final Set<Object> services = Set.of(config);

    final MockServiceLocator serviceLocator = (MockServiceLocator) ServiceLocator.create();
    serviceLocator.setServices(services);

    q2TRestApp = new Q2TRestApp();

    jersey =
        new JerseyTest() {
          @Override
          protected Application configure() {
            enable(TestProperties.LOG_TRAFFIC);
            enable(TestProperties.DUMP_ENTITY);
            return ResourceConfig.forApplication(q2TRestApp);
          }
        };

    jersey.setUp();
  }

  @After
  public void tearDown() throws Exception {
    jersey.tearDown();
  }

  @Test
  public void getSingletons() {
    assertThat(q2TRestApp.getSingletons()).hasSize(4);
  }

  @Test
  public void appType() {
    assertThat(q2TRestApp.getAppType()).isEqualTo(AppType.Q2T);
  }
}
