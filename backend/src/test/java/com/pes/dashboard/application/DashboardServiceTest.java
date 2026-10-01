package com.pes.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pes.dashboard.api.DashboardDtos;
import com.pes.plan.domain.ProductionPlanRepository;
import com.pes.plan.domain.ProductionPlanStatus;
import com.pes.result.domain.ProductionResultRepository;
import com.pes.workorder.domain.WorkOrderRepository;
import com.pes.workorder.domain.WorkOrderStatus;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	private ProductionPlanRepository productionPlanRepository;

	@Mock
	private WorkOrderRepository workOrderRepository;

	@Mock
	private ProductionResultRepository productionResultRepository;

	@Test
	void aggregatesPlanWorkOrderAndProductionQuantitiesConsistently() {
		when(productionPlanRepository.countByStatus(ProductionPlanStatus.CONFIRMED)).thenReturn(2L);
		when(productionPlanRepository.sumTargetQuantityByStatus(ProductionPlanStatus.CONFIRMED)).thenReturn(200L);
		when(workOrderRepository.sumTargetQuantity()).thenReturn(200L);
		when(workOrderRepository.countByStatus(WorkOrderStatus.WAITING)).thenReturn(1L);
		when(workOrderRepository.countByStatus(WorkOrderStatus.IN_PROGRESS)).thenReturn(1L);
		when(workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED)).thenReturn(2L);
		ProductionResultRepository.QuantitySummary resultSummary = mock(
				ProductionResultRepository.QuantitySummary.class);
		when(resultSummary.getProducedQuantity()).thenReturn(160L);
		when(resultSummary.getGoodQuantity()).thenReturn(152L);
		when(resultSummary.getDefectQuantity()).thenReturn(8L);
		when(productionResultRepository.summarizeAll()).thenReturn(resultSummary);
		DashboardService service = new DashboardService(
				productionPlanRepository, workOrderRepository, productionResultRepository);

		DashboardDtos.Response response = service.getSummary();

		assertThat(response.workOrders().total()).isEqualTo(4);
		assertThat(response.quantities().planned()).isEqualTo(response.quantities().ordered());
		assertThat(response.quantities().produced())
				.isEqualTo(response.quantities().good() + response.quantities().defect());
		assertThat(response.planAchievementRate()).isEqualTo(80.0);
		assertThat(response.goodRate()).isEqualTo(95.0);
	}
}
