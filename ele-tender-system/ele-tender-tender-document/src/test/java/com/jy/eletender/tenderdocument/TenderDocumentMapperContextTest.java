package com.jy.eletender.tenderdocument;

import com.jy.eletender.tenderdocument.mapper.ProjectLockMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TenderDocumentApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:tender_document;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
        }
)
class TenderDocumentMapperContextTest {

    @MockBean
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private ProjectLockMapper projectLockMapper;

    @Autowired
    private TenderDocumentMapper tenderDocumentMapper;

    @Autowired
    private TenderRuleHeaderMapper tenderRuleHeaderMapper;

    @Test
    void shouldRegisterCoreMapperBeans() {
        assertThat(projectLockMapper).isNotNull();
        assertThat(tenderDocumentMapper).isNotNull();
        assertThat(tenderRuleHeaderMapper).isNotNull();
    }
}
