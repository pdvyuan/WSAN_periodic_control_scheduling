function [feasibles, saveRate, rate] = compareRepSch(implicit, physical, piggyback, toPlot)
    if (nargin == 2)
        toPlot = 1;
    end
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
    
    if (piggyback)
        str = [str, ', opportunistic aggregation'];
    else
        str = [str, ', w/o opportunistic aggregation'];
    end
    
    file = '../log/revision/shortSchedule/';
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
    data = data(data(:, 7) == piggyback, :);
    data(data == -1) = 0;
    
    feasibles = [];
    for repeat = 0:1
        dataP = data(find(data(:, 4) == repeat), :);
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
        legend('hyper-period scheduling', 'repetitive scheduling', 'location', 'NorthWest');
        set(gca,'XTickLabel', {'1', '2', '4', '8', '16'}, 'FontSize', 15);
        xlabel('number of channels');
    end
    
    schedulableRepetitive = data(data(:, 4) == 1 & data(:, 8) == 1, :);
    saveRate = schedulableRepetitive(:, 16)./schedulableRepetitive(:, 15);
    h = figure;
    set(h, 'position',[100 100 700 350]);
    plot(schedulableRepetitive(:, 15), schedulableRepetitive(:, 16), '.');
    max(schedulableRepetitive(:, 15))
    max(schedulableRepetitive(:, 16))
    %axis equal;
    xs = axis;
    %axis([0, 150000, xs(3), xs(4)]);
    xlabel('hyper-period scheduling');
    ylabel('repetitive scheduling');
    set(gca, 'FontSize', 13);
    %title(str);
    if (toPlot)
        figure
        boxplot(saveRate);
    
        f1 = figure;
        hold on;
        title('nonempty slots vs time');

        f2 = figure;
        hold on;
        title('trans vs time');

        f3 = figure;
        hold on;

        f4 = figure;
        hold on;
    end
    
    %hyper-period scheduling and feasible
    pdata = data(data(:, 4)==0 & data(:, 8)==1, :);
    
    if (toPlot)
        figure(f1);
        plot(pdata(:, 17), pdata(:, 11), 'r.');
        figure(f2)
        plot(pdata(:, 15), pdata(:, 11), 'r.');
    end
    
    
    trans = sort(unique(pdata(:, 15)));
    meanTimes = zeros(size(trans));
    
    for i=1:length(trans)
        tx = trans(i);
        meanTimes(i) = mean(pdata(pdata(:, 15) == tx, 11))/10^6;
    end
    
    if (toPlot)
        figure(f3);
        plot(pdata(:, 15), pdata(:, 11) / 10^6, 'r+');
        %disp('max')
        %max(pdata(:, 11))
        %plot(trans, meanTimes, 'r+');
        %csvwrite('d1.csv', [pdata(:, 15), pdata(:, 11)/ 10^6]);

        figure(f4);
        plot(pdata(:, 15), pdata(:, 11)./pdata(:, 15), 'r.');
    end
    
    %repetitive scheduling and feasible
    pdata = data(data(:, 4)==1 & data(:, 8)==1, :);
    
    if (toPlot)
        figure(f1);
        plot(pdata(:, 17), pdata(:, 11), 'b.');
        figure(f2)
        plot(pdata(:, 15), pdata(:, 11), 'b.');
    end

    trans = sort(unique(pdata(:, 15)));
    meanTimes = zeros(size(trans));
    
    for i=1:length(trans)
        tx = trans(i);
        meanTimes(i) = mean(pdata(pdata(:, 15) == tx, 11)) / 10^6;
    end
    
    if (toPlot)
        figure(f3);
        set(gca, 'FontSize', 15);
        plot(pdata(:, 15), pdata(:, 11) / 10^6, 'b.');
        %csvwrite('d2.csv', [pdata(:, 15), pdata(:, 11)/ 10^6]);
        %disp('max')
        %max(pdata(:, 11))
        %plot(trans, meanTimes, 'b.');
        xs = axis;
        axis([xs(1), 150000, xs(3), xs(4)]);
        xlabel('transmissions in a hyper-period');
        ylabel('execution time (ms)');
        legend('hyper-period scheduling', 'repetitive scheduling', 'location', 'NorthWest');
        %title(str);

        figure(f4);
        plot(pdata(:, 15), pdata(:, 11)./pdata(:, 16), 'b.');
        
    end
    
    rates = [];
    for i=1:size(data, 1)/2
        if (data(2*i-1, 8) == 1 && data(2*i, 8) == 1)
            ht = data(2*i-1, 11);
            rt = data(2*i, 11);
            rates = [rates, rt/ht];
        end
    end
    rate = median(rates);
end

function data = extract_data(file)
    x = importdata(file);
    data = x.data;
end
