function [feasibles, oaPercent, times, maxAggregate] = compareOA(implicit, physical, toPlot)
    if (nargin == 2)
        toPlot = 1;
    end
    maxutil = 25;
    if (implicit)
        str = 'implicit deadline';
    else
        str = 'restricted deadline';
    end
    
    if (physical)
        str = [str];
    else
        str = [str ', disk model'];
    end
    
    file = '../log/revision/oppor_aggr/';
    if (implicit)
        file = [file, 'implicit/'];
    else
        file = [file, 'restricted/'];
    end
    if (physical)
        file = [file, 'physical/'];
    else
        file = [file, 'random/'];
    end
    file = [file, 'convert.txt'];
    
    data = extract_data(file);
    data(data == -1) = 0;
    
    feasibles = [];
    for pig = 0:1
        dataP = data(find(data(:, 7) == pig), :);
        fRow = [];
        for i=0:4
            ch = 2^i;
            dataC = dataP(dataP(:, 6) == ch, :);
            fRow = [fRow, sum(dataC(:, 8))./size(dataC, 1)];
        end
        feasibles = [feasibles; fRow];
    end
    feasibles = feasibles * 100;
    if (toPlot)
        figure;
        bar(feasibles');
        %title(str);
        ylabel('schedulability ratio (%)');
        legend('w/o oa', 'with oa', 'location', 'NorthWest');
        set(gca,'XTickLabel', {'1', '2', '4', '8', '16'}, 'FontSize', 15);
        xlabel('number of channels');
        if (~implicit)
            axis([0 6 0 50]);
        end
    end

    
    lineType = '-';
    for pig = 0:1
        if (toPlot)
            fig = figure;
            hold on;
            set(fig, 'Position', [200 500 700 350]);
        end

        dataPig = data(data(:, 7) == pig, :);
        
        stepLen = 0.5;
        channels = dataPig(:, 6);
        utilmat = [];
        for i=0:4
            ch = 2^i;
            %pdata contains floored utils and feasible.
            pdata = dataPig(channels == ch, [5, 8]);
            utilbar = [];
            minV = 0;
            while (minV + stepLen <= maxutil)
                x = pdata((pdata(:, 1) > minV & pdata(:, 1) <= (minV+stepLen)), 2);
                if (length(x) > 0) 
                    utilbar = [utilbar, sum(x) / length(x)];
                else
                    utilbar = [utilbar, 0];
                end

                minV = minV + stepLen;
            end
            utilmat = [utilmat; utilbar];
        end
        utilmat = utilmat * 100;
       
        if (toPlot)
            xs = stepLen/2:stepLen:maxutil;
            for i=1:size(utilmat, 1)
                ys = utilmat(i, :);
                if (i == 1)
                    pointType = '+';
                elseif (i == 2)
                    pointType = 'o';
                elseif (i == 3)
                    pointType = 'x';
                elseif (i == 4)
                    pointType = 's';
                else
                    pointType = '*';
                end
                plot(xs, ys, [lineType, pointType]);
            end
            xlabel('total utilization');
            ylabel('schedulability ratio (%)');
            legend('1 ch.', '2 ch.', '4 ch.', '8 ch.', '16 ch.');
            set(gca,'XTick',0:2:maxutil, 'FontSize', 15);
            grid on;
        
            if (pig)
                s = ' (w. oa)';
            else
                s = ' (w/o oa)';
            end
            %title([str, s]);    
        end
    end
    
    times = zeros(1, 2);
    nPoints = 0;
    for i=1:2:size(data, 1)
        if (data(i, 8) == 1 && data(i+1, 8) == 1)
            nPoints = nPoints+1;
            times(1) = times(1) + data(i, 11);
            times(2) = times(2) + data(i+1, 11);
        end
    end
    times = times / nPoints;
    
    oaPer = data(data(:, 7) == 1 & data(:, 8) == 1, 9);
    oaPercent = mean(oaPer);
    maxAggregate = max(data(:, 10));
end

function data = extract_data(file)
    x = importdata(file);
    data = x.data;
end
