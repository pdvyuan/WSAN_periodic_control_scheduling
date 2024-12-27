function [metrics, metricsErr] = compare3PlacementVariants(physical, metric)
    metrics = [];
    metricsErr = [];
    for numGW = 2:2:6
        [m, e] = effectOfGWPlacementOnRouting(physical, numGW, metric);
        m = m(:, 2:5);
        e = e(:, 2:5);
        metrics = [metrics, m];
        metricsErr = [metricsErr, e];
    end
    if (strcmp(metric, 'f') == 1)
        metrics = metrics * 100;
        metricsErr = metricsErr * 100;
    end
    fig = figure;
    set(fig, 'Position', [200 200 1000 300]);
    barwitherr(metricsErr', metrics');
    set(gca,'XTickLabel', {'GNP/2', 'LNP/2', 'GMC/2', 'LMC/2',...
        'GNP/4', 'LNP/4', 'GMC/4', 'LMC/4', 'GNP/6', 'LNP/6', 'GMC/6', 'LMC/6'});
    legend('all nodes', 'ends', 'weighted ends', 'location', 'SouthEast');
    if (physical)
        titleStr = 'realistic link quality model';
    else
        titleStr = 'random link quality model';
    end
    title(titleStr);
    if (strcmp(metric, 'wr') == 1)
        ylabel('mean e2e packet reliability');
    elseif (strcmp(metric, 'wh') == 1)
        ylabel('mean e2e packet comm cost');
    elseif (strcmp(metric, 'f') == 1)
        ylabel('mean feasibility percentage (%)');
    end
    if (physical && strcmp(metric, 'wr') == 1)
        axis([0, 14, 0.94, 1.05]);
    end
    if (physical && strcmp(metric, 'f') == 1)
        axis([0, 14, 80, 110]);
    end
    if ((physical == 0) && strcmp(metric, 'f') == 1)
        axis([0, 14, 60, 110]);
    end
end