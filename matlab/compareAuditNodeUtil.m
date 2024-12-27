function compareAuditNodeUtil
    file = 'D:\pdv\workspace\realtime-tdma\log\redo\routing\auditNodeUtil\convert.txt';
    data = extract_data(file);
    maxUtils = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 0.0];
    feasibles = zeros(size(maxUtils));
    for gw = 1:2
        numGWs = 2*gw;
        if (gw == 1)
            lineType = '--';
        else
            lineType = '-';
        end
        for routing = [1, 2, 3, 4, 5, 6, 7, 8, 9]
            if (routing == 1)
                marker = 'o';
            elseif (routing == 2)
                marker = '*';
            elseif (routing == 3)
                marker = '.';
            elseif (routing == 4)
                marker = 's';
            elseif (routing == 5)
                marker = 'd';
            elseif (routing == 6)
                marker = '+';
            elseif (routing == 7)
                marker = '^';
            elseif (routing == 8)
                marker = 'v';
            else
                marker = 'x';
            end
            for i=1:length(maxUtils)
                maxUtil = maxUtils(i);
                pdata = data(data(:, 2) == numGWs & data(:, 4) == routing & data(:, 10) == maxUtil, :);
                f = sum(sum(pdata(:, 12:27), 2) > 0) / size(pdata, 1);
                %f = sum(pdata(:, 5) == pdata(:, 6)) / size(pdata, 1);
                %f = sum(sum(pdata(:, 12:27), 2) > 0) / sum(pdata(:, 5) == pdata(:, 6));
                feasibles(i) = f;
            end
            plot([maxUtils(1:length(maxUtils)-1), 1.2], feasibles, 'LineStyle', lineType, 'Marker', marker);
            hold on;
            fprintf('%d GWs, routing %d\n', numGWs, routing);
            feasibles
        end
    end
    legend('2 GWs, most-reliable', '2 GWs, shortest', '2 GWs, shortest-dinic', '2 GWs, min-sum-loss-rate',...
        '2 GWs, max-prod-rec-rate', '2 GWs, min-sum-etx', '2 GWs, min-sum-hops', '2 GWs, most-reliable-lc', '2 GWs, most-reliable-bf', ...
        '4 GWs, most-reliable', '4 GWs, shortest', '4 GWs, shortest-dinic', '4 GWs, min-sum-loss-rate',...
        '4 GWs, max-prod-rec-rate', '4 GWs, min-sum-etx', '4 GWs, min-sum-hops', '4 GWs, most-reliable-lc', '4 GWs, most-reliable-bf');
    set(gca,'XTick', [maxUtils(1:length(maxUtils)-1), 1.2]);
    set(gca,'XTickLabel', {'0.1', '0.2', '0.3', '0.4', '0.5', '0.6', '0.7', '0.8', '0.9', '1.0', '+inf'});
    xlabel('max node util');
    ylabel('schedulable rate');
    
%     minFeasibleChannels = zeros(1, 5);
%     for i=1:size(data, 1)/5
%         for j=1:5
%             ch = 0;
%             row = 5*(i-1)+j;
%             for k=1:16
%                 if (data(row, 11+k) == 1)
%                     ch = k;
%                     break;
%                 end
%             end
%             minFeasibleChannels(j) = ch;
%         end
%         if (checkSame(minFeasibleChannels) == 0)
%             disp(minFeasibleChannels);
%         end
%     end
end

function same=checkSame(f)
    for i=1:length(f)-1
        if (f(i) ~= f(i+1))
            same = 0;
            return;
        end
    end
    same = 1;
end

function data = extract_data(file)
    x = importdata(file);
    data = x.data;
end