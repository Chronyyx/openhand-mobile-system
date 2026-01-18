package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.businesslayer.AuditLogService;
import com.mana.openhand_backend.identity.dataaccesslayer.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "openhand.app.jwtSecret=testSecret", "openhand.app.jwtRefreshExpirationMs=86400000" })
@AutoConfigureMockMvc
class AuditLogControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuditLogService auditLogService;

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAuditLogs_adminUser_logsAccess() throws Exception {
                when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                                .thenReturn(new PageImpl<>(Collections.emptyList()));

                mockMvc.perform(get("/api/admin/audit-logs"))
                                .andExpect(status().isOk());

                verify(auditLogService).logAccess(any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAuditLogs_skipLogTrue_doesNotLogAccess() throws Exception {
                when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                                .thenReturn(new PageImpl<>(Collections.emptyList()));

                mockMvc.perform(get("/api/admin/audit-logs")
                                .param("skipLog", "true"))
                                .andExpect(status().isOk());

                // Verify logAccess was NOT called
                verify(auditLogService, org.mockito.Mockito.never()).logAccess(any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(roles = "MEMBER")
        void getAuditLogs_memberUser_returns403() throws Exception {
                mockMvc.perform(get("/api/admin/audit-logs"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void exportAuditLogs_returnsCsvFile() throws Exception {
                when(auditLogService.exportAuditLogsToCsv(any(), any(), any(), any())).thenReturn("csv,data");

                mockMvc.perform(get("/api/admin/audit-logs/export"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Disposition",
                                                "attachment; filename=audit-logs.csv"));
        }
}
