function [metrics, metricsE] = effectOfGWPlacementOnRouting(physical, numGWs, metricType)
    file = 'D:\pdv\workspace\realtime-tdma\log\redo\placeGWs\';
    if (physical)
        file = [file, 'physical\convert.txt'];
    else
        file = [file, 'random\convert.txt'];
    end
    data = extract_data(file);
    data = data(find(data(:, 2) == numGWs), :);
    
    metrics = [];
    metricsE = [];
    for subtype = 0:1:2
        aData = data(find(data(:, 4) == subtype), :);
        metricsRow = [];
        metricsERow = [];
        for place = 0:1:6
            tData = aData(find(aData(:, 3) == place), :);
            if (strcmp(metricType, 'f')) 
                values = tData(:, 7)./tData(:, 6);
            elseif strcmp(metricType, 'r')
                values = tData(:, 8);
            elseif strcmp(metricType, 'wr')
                values = tData(:, 9);
            elseif strcmp(metricType, 'h')
                values = tData(:, 10);
            else
                values = tData(:, 11);
            end
            metricsRow = [metricsRow, mean(values)]; %feasibility
            metricsERow = [metricsERow, std(values)];
            
        end
        metrics = [metrics; metricsRow];
        metricsE = [metricsE; metricsERow];
    end
%     routablePercent = [routablePercent; routableRow];
%     routablePercentE = [routablePercentE; routableERow];
% 
%     reliability = [reliability; reliabilityRow];
%     reliabilityE = [reliabilityE; reliabilityERow];
% 
%     hops = [hops; hopsRow];
%     hopsE = [hopsE; hopsERow];
% 
%     routablePercent = routablePercent * 100;
%     routablePercentE = routablePercentE * 100;
%     barwitherr(routablePercentE, routablePercent);
%     ylabel('mean percentage of flows with 2-disjoint-paths (%)');
%     legend('sector-center', 'min-sum-dist', 'cluster-min-sum-dist', ...
%         'min-sum-square-dist', 'cluster-min-sum-square-dist', 'center');
%     
%     figure;
%     barwitherr(reliabilityE, reliability);
%     set(gca,'XTickLabel', {'unreliable GWs', 'reliable GWs'});
%     ylabel('mean flow reliability');
%     legend('sector-center', 'min-sum-dist', 'cluster-min-sum-dist', ...
%         'min-sum-square-dist', 'cluster-min-sum-square-dist', 'center');
%     
%     figure;
%     barwitherr(hopsE, hops);
%     set(gca,'XTickLabel', {'unreliable GWs', 'reliable GWs'});
%     ylabel('mean flow total hops');
%     legend('sector-center', 'min-sum-dist', 'cluster-min-sum-dist', ...
%         'min-sum-square-dist', 'cluster-min-sum-square-dist', 'center');
end

function data = extract_data(file)
    x = importdata(file);
    data = x.data;
end