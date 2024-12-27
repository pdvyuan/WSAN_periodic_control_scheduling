function [metrics, metricsErr] = comparePlacement(physical, metric)
    metrics = [];
    metricsErr = [];
    for numGW = 2:2:6
        [m, e] = effectOfGWPlacementOnRouting(physical, numGW, metric);
        m = [m(1, 1), m(3, 2:5), m(1, 6)];
        e = [e(1, 1), e(3, 2:5), e(1, 6)];
        metrics = [metrics; m];
        metricsErr = [metricsErr; e];
    end
    metrics = metrics(:, [6, 1, 2, 3, 4, 5]);
    metricsErr = metricsErr(:, [6, 1, 2, 3, 4, 5]);
    fig = figure;
    set(fig, 'Position', [200 200 1000 400]);
    
    if (strcmp(metric, 'f') == 1)
        metrics = metrics * 100;
        metricsErr = metricsErr * 100;
    end
   
    barwitherr(metricsErr, metrics);
    set(gca,'XTickLabel', {'2 GWs', '4 GWs', '6 GWs'});
    legend('center', 'grid center', 'global nearest point', 'local nearest point', ...
        'global mass center', 'local mass center', 'Location', 'SouthWest');
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
    
    if (physical && strcmp(metric, 'f') == 1)
        v = axis;
        v(3) = 80;
        v(4) = 105;
        axis(v);
    end
    
    if (physical && strcmp(metric, 'wr') == 1)
        v = axis;
        v(3) = 0.9;
        v(4) = 1.05;
        axis(v);
    end
    
end